package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.KeySharingPossible
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.NoKeySharingPossible
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ShowShareKeysReminderNotificationIfNeededTest {

    private val notificationProvider = mockk<NotificationProvider>(relaxUnitFun = true)
    private val keySharingInfoProvider = mockk<KeySharingInfoProvider>(relaxUnitFun = true)
    private val canShareKeys = mockk<CanShareKeys>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-28T00:05:00.00Z"), ZoneOffset.UTC)

    private val keySharingInfo = mockk<KeySharingInfo>()

    private val showShareKeysReminderNotificationIfNeeded = ShowShareKeysReminderNotificationIfNeeded(
        notificationProvider,
        keySharingInfoProvider,
        canShareKeys,
        fixedClock
    )

    @Test
    fun `if key sharing is not possible do nothing`() {
        every { canShareKeys() } returns NoKeySharingPossible

        showShareKeysReminderNotificationIfNeeded()

        thenNotificationIsNotSent()
    }

    @Test
    fun `if test result was acknowledged less than 24h ago do nothing`() {
        every { canShareKeys() } returns KeySharingPossible(keySharingInfo)
        every { keySharingInfo.wasAcknowledgedMoreThan24HoursAgo(fixedClock) } returns false
        every { keySharingInfo.notificationSentDate } returns null

        showShareKeysReminderNotificationIfNeeded()

        thenNotificationIsNotSent()
    }

    @Test
    fun `if notification was already shown do nothing`() {
        every { canShareKeys() } returns KeySharingPossible(keySharingInfo)
        every { keySharingInfo.wasAcknowledgedMoreThan24HoursAgo(fixedClock) } returns true
        every { keySharingInfo.notificationSentDate } returns mockk()

        showShareKeysReminderNotificationIfNeeded()

        thenNotificationIsNotSent()
    }

    @Test
    fun `show notification`() {
        every { canShareKeys() } returns KeySharingPossible(keySharingInfo)
        every { keySharingInfo.wasAcknowledgedMoreThan24HoursAgo(fixedClock) } returns true
        every { keySharingInfo.notificationSentDate } returns null

        showShareKeysReminderNotificationIfNeeded()

        thenShareKeysNotificationIsSent()
    }

    private fun thenShareKeysNotificationIsSent() {
        verify { keySharingInfoProvider.setNotificationSentDate(Instant.now(fixedClock)) }
        verify { notificationProvider.showShareKeysReminderNotification() }
    }

    private fun thenNotificationIsNotSent() {
        verify(exactly = 0) { keySharingInfoProvider.setNotificationSentDate(any()) }
        verify(exactly = 0) { notificationProvider.showShareKeysReminderNotification() }
    }
}
