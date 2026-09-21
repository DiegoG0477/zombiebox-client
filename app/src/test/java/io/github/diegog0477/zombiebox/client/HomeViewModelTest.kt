package io.github.diegog0477.zombiebox.client

import org.junit.Assert.*
import org.junit.Test
import io.github.diegog0477.zombiebox.client.model.*
import io.github.diegog0477.zombiebox.client.presentation.HomeViewModel

class HomeViewModelTest {
    private class Repository : HomeRepository {
        var fail = false
        override fun load(scope: HomeScope): HomeSnapshot {
            if (fail) throw IllegalStateException("offline")
            return HomeSnapshot(hero = MediaItem(scope.provider, scope.provider, scope.query))
        }
    }
    @Test fun staleRequestsCannotReplaceNewSelection() {
        val work = ArrayList<() -> Unit>()
        val vm = HomeViewModel(Repository(), { work.add(it) }, { it() })
        vm.refresh(HomeScope("plex")); vm.refresh(HomeScope("iptv"))
        work[1](); work[0]()
        assertEquals("iptv", vm.state.snapshot.hero?.id)
        vm.refresh(); work[2]()
        assertEquals("iptv", vm.state.scope.provider)
    }
    @Test fun failureKeepsContentAndCloseDropsLateCallbacks() {
        val repository = Repository()
        val work = ArrayList<() -> Unit>()
        val vm = HomeViewModel(repository, { work.add(it) }, { it() })
        vm.refresh(HomeScope("local")); work.removeAt(0)()
        repository.fail = true
        vm.refresh(); work.removeAt(0)()
        assertEquals("local", vm.state.snapshot.hero?.id)
        assertNotNull(vm.state.failure)
        var notifications = 0
        vm.observer = { notifications++ }
        vm.refresh(); vm.close(); work.removeAt(0)()
        assertEquals(1, notifications)
        assertNull(vm.observer)
    }
    @Test fun resetInvalidatesPreviousGatewayResults() {
        val work = ArrayList<() -> Unit>()
        val vm = HomeViewModel(Repository(), { work.add(it) }, { it() })
        vm.refresh(HomeScope("old")); vm.reset(); work[0]()
        assertNull(vm.state.snapshot.hero)
    }
}
