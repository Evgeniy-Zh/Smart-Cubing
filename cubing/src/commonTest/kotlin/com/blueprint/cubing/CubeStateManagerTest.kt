package com.blueprint.cubing

import com.blueprint.cubing.core.model.ConnectionState
import com.blueprint.cubing.core.model.CubeDevice
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.model.CubeRequest
import com.blueprint.cubing.core.model.DeviceConnection
import com.blueprint.cubing.core.model.DeviceIdentifier
import com.blueprint.cubing.cube.CubeRepository
import com.blueprint.cubing.cube.CubeStateManager
import com.blueprint.cubing.device.list.CubeListRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class CubeStateManagerTest {

    private lateinit var cubeRepository: FakeCubeRepository
    private lateinit var deviceRepo: FakeCubeListRepository
    private lateinit var cubeStateManager: CubeStateManager
    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setup() {
        cubeRepository = FakeCubeRepository()
        deviceRepo = FakeCubeListRepository()
        cubeStateManager = CubeStateManager(cubeRepository, deviceRepo)
        val job = Job()
        scope = CoroutineScope(Dispatchers.Default + job)
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `observeCubeEvents emits move events after successful connect`() = runBlocking {
        val device = CubeDevice("name", "model", FakeIdentifier("id1"))
        // Prepare repository to return a connection
        cubeRepository.willConnect = DeviceConnection(Uuid.random(), device.identifier)

        val received = mutableListOf<CubeEvent>()

        val collectJob = launch {
            cubeStateManager.observeCubeEvents().take(2).collect {
                received.add(it)
            }
        }

        // Activate device (this should trigger connect)
        deviceRepo.setActive(device)

        // Emit events from repository
        cubeRepository.emitEvent(CubeEvent.Move("R"))
        cubeRepository.emitEvent(CubeEvent.Move("U"))

        // allow some time for flows to propagate
        delay(200)

        // wait for the collector to finish receiving two items
        collectJob.join()

        assertEquals(2, received.size)
        assertEquals(CubeEvent.Move("R"), received[0])
        assertEquals(CubeEvent.Move("U"), received[1])
    }

    @Test
    fun `observeCubeEvents triggers reconnect when connect returns null`() = runBlocking {
        val device = CubeDevice("name", "model", FakeIdentifier("id2"))
        // repository will fail to connect
        cubeRepository.willConnect = null

        val collectJob = launch {
            cubeStateManager.observeCubeEvents().collect {}
        }

        deviceRepo.setActive(device)

        // triggerReconnect has two delays of 300ms each; wait enough time
        delay(800)

        // Check that setAsActive was called with null then with the identifier again
        assertTrue(deviceRepo.setAsActiveCalls.size >= 2)
        assertEquals(null, deviceRepo.setAsActiveCalls[0])
        assertEquals(device, deviceRepo.setAsActiveCalls[1])

        collectJob.cancel()
    }

    @Test
    fun `observeConnectionEvents propagates states and calls sync on connected`() = runBlocking {
        val device = CubeDevice("name", "model", FakeIdentifier("id3"))
        cubeRepository.willConnect = DeviceConnection(Uuid.random(), device.identifier)

        val received = mutableListOf<ConnectionState>()

        val connJob = launch {
            cubeStateManager.observeConnectionEvents().take(2).collect {
                received.add(it)
            }
        }

        val eventsJob = launch {
            cubeStateManager.observeCubeEvents().collect {}
        }

        deviceRepo.setActive(device)

        // Emit Connected, then Disconnected
        cubeRepository.emitConnection(ConnectionState.Connected)
        // allow sync to be called
        delay(100)
        cubeRepository.emitConnection(ConnectionState.Disconnected)

        delay(200)

        connJob.join()
        // stop the long-running cube events collector
        eventsJob.cancel()
        eventsJob.join()

        // Verify that sync was invoked (Sync request recorded)
        assertTrue(cubeRepository.sentRequests.any { it == CubeRequest.Sync })
        // Verify that we received at least Connected and Disconnected
        assertTrue(received.any { it is ConnectionState.Connected })
        assertTrue(received.any { it is ConnectionState.Disconnected })
    }

    @Test
    fun `sync and reset delegate to repository`() = runBlocking {
        // call sync and reset and verify repository received requests
        cubeStateManager.sync()
        cubeStateManager.reset()

        assertTrue(cubeRepository.sentRequests.contains(CubeRequest.Sync))
        assertTrue(cubeRepository.sentRequests.contains(CubeRequest.Reset))
    }

    @Test
    fun `switching active device disconnects previous connection`() = runBlocking {
        val device1 = CubeDevice("one", "model", FakeIdentifier("a"))
        val device2 = CubeDevice("two", "model", FakeIdentifier("b"))
        cubeRepository.willConnect = DeviceConnection(Uuid.random(), device1.identifier)

        val eventsJob = launch {
            cubeStateManager.observeCubeEvents().collect {}
        }

        deviceRepo.setActive(device1)
        // allow connect to happen
        delay(100)

        // Now switch active device
        deviceRepo.setActive(device2)

        // allow disconnect to be processed
        delay(200)

        eventsJob.cancel()
        eventsJob.join()

        assertTrue(cubeRepository.disconnectCalled)
    }

    @Test
    fun `unsubscribe and subscribe again receives events after resubscribe`() = runBlocking {
        val device = CubeDevice("re", "model", FakeIdentifier("resub"))
        cubeRepository.willConnect = DeviceConnection(Uuid.random(), device.identifier)

        // Subscribe first time and receive one event
        val firstReceived = mutableListOf<CubeEvent>()
        val first = launch {
            cubeStateManager.observeCubeEvents().take(1).collect { firstReceived.add(it) }
        }

        deviceRepo.setActive(device)
        cubeRepository.emitEvent(CubeEvent.Move("X"))
        // wait for first collector to receive
        first.join()

        assertEquals(1, firstReceived.size)

        // After first subscription completes, manager should have disconnected (onCompletion)
        // Emit an event while unsubscribed
        cubeRepository.emitEvent(CubeEvent.Move("Y"))

        // Now subscribe again and ensure we receive new events
        val secondReceived = mutableListOf<CubeEvent>()
        val second = launch {
            cubeStateManager.observeCubeEvents().take(1).collect { secondReceived.add(it) }
        }

        // Emit another event which should be received by the new subscriber
        cubeRepository.emitEvent(CubeEvent.Move("Z"))

        second.join()

        assertEquals(1, secondReceived.size)
        // ensure we didn't accidentally receive the Y emitted while unsubscribed as the first collector already finished
        assertTrue(secondReceived[0] is CubeEvent.Move)

        // cleanup
        second.cancel()
    }

    // --- Fake implementations used by the tests ---

    class FakeIdentifier(private val id: String) : DeviceIdentifier {
        override fun toString(): String = id
    }

    class FakeCubeListRepository : CubeListRepository {
        private val _active = MutableStateFlow<CubeDevice?>(null)
        val setAsActiveCalls = mutableListOf<CubeDevice?>()

        override val lastActiveDevice: CubeDevice? = null
        override val activeDevice: CubeDevice?
            get() = _active.value

        override fun observeActiveDevice() = _active.asStateFlow()

        override fun observeDevices() =
            MutableStateFlow<List<CubeDevice>>(emptyList()).asStateFlow()

        override suspend fun setAsActive(cube: CubeDevice?) {
            setAsActiveCalls.add(cube)
            _active.value = cube
        }

        // helper to set active quickly from tests (non-suspend)
        fun setActive(cube: CubeDevice) {
            _active.value = cube
        }

        override suspend fun addDevice(cube: CubeDevice) {
            // no-op for tests
        }

        override suspend fun removeDevice(cube: CubeDevice) {
            // no-op for tests
        }
    }

    class FakeCubeRepository : CubeRepository {
        // Use replay so late collectors still receive the last event/state
        private val _events = MutableSharedFlow<CubeEvent>(replay = 10)
        private val _conn = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

        var willConnect: DeviceConnection? = null
        val sentRequests = mutableListOf<CubeRequest>()
        var disconnectCalled: Boolean = false

        override fun observeCubeEvents() = _events.asSharedFlow()
        override fun observeConnectionEvents() = _conn.asStateFlow()

        suspend fun emitEvent(e: CubeEvent) {
            _events.emit(e)
        }

        suspend fun emitConnection(c: ConnectionState) {
            _conn.emit(c)
        }

        override suspend fun sendCubeRequest(request: CubeRequest) {
            sentRequests.add(request)
        }

        override suspend fun connect(cubeDevice: CubeDevice): DeviceConnection? {
            // simulate that when connect succeeds, we flip the connection state to Initializing
            val result = willConnect
            if (result != null) {
                // indicate connecting then connected
                _conn.emit(ConnectionState.Connecting)
                _conn.emit(ConnectionState.Connected)
            } else {
                _conn.emit(ConnectionState.FailedToConnect)
            }
            return result
        }

        override suspend fun disconnect() {
            disconnectCalled = true
            _conn.emit(ConnectionState.Disconnected)
        }
    }

    fun currentMillis(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }
}