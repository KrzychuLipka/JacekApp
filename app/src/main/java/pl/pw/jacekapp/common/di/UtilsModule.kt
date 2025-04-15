package pl.pw.jacekapp.common.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Region
import pl.pw.jacekapp.common.utils.BeaconUtils
import pl.pw.jacekapp.common.utils.ErrorHandler
import pl.pw.jacekapp.common.utils.Logger
import pl.pw.jacekapp.common.utils.QrCodeScannerUtils
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilsModule {

    private const val BEACONS_REGION_UNIQUE_ID = "all-beacons-region"

    @Provides
    @Singleton
    fun provideLogger(
        @ApplicationContext context: Context
    ): Logger = Logger(context)

    @Provides
    @Singleton
    fun provideErrorHandler(
        @ApplicationContext context: Context
    ): ErrorHandler = ErrorHandler(context)

    @Provides
    @Singleton
    fun provideQrCodeScannerUtils(
        @ApplicationContext context: Context
    ): QrCodeScannerUtils = QrCodeScannerUtils(context)

    @Provides
    @Singleton
    fun provideBeaconRegion(): Region =
        Region(BEACONS_REGION_UNIQUE_ID, null, null, null)

    @Provides
    @Singleton
    fun provideBeaconManager(
        @ApplicationContext context: Context
    ): BeaconManager =
        BeaconManager.getInstanceForApplication(context).apply {
            listOf(
                BeaconParser.EDDYSTONE_UID_LAYOUT,
                BeaconParser.EDDYSTONE_TLM_LAYOUT,
                BeaconParser.EDDYSTONE_URL_LAYOUT,
            ).forEach {
                beaconParsers.add(BeaconParser().setBeaconLayout(it))
            }
        }

    @Provides
    @Singleton
    fun provideBeaconUtils(
        beaconRegion: Region,
        beaconManager: BeaconManager
    ): BeaconUtils = BeaconUtils(beaconRegion, beaconManager)
}
