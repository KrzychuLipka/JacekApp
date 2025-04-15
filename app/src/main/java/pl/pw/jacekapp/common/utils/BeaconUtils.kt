package pl.pw.jacekapp.common.utils

import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import javax.inject.Inject

class BeaconUtils @Inject constructor(
    private val beaconRegion: Region,
    private val beaconManager: BeaconManager,
) {

    fun listenForScannedBeacons(
        callback: (List<Beacon>) -> Unit,
    ) {
        beaconManager.addRangeNotifier { beacons, _ ->
            callback(beacons.toList())
        }
    }

    fun startRangingBeacons() {
        beaconManager.startRangingBeacons(beaconRegion)
    }

    fun stopRangingBeacons() {
        beaconManager.stopRangingBeacons(beaconRegion)
    }
}
