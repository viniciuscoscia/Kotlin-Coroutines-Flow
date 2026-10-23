package lab
import kotlin.test.*
class ImportOwnershipTest {
 @Test fun onlyOneImportOwnsThePickerAndLateCompletionCannotFinishAnother() {
  val ownership=ImportOwnership(); val first=ownership.begin()!!
  assertNull(ownership.begin()); assertTrue(ownership.busy)
  assertTrue(ownership.finish(first)); val second=ownership.begin()!!
  assertFalse(ownership.finish(first)); assertTrue(ownership.isCurrent(second))
 }
 @Test fun cancelledPickerAlreadySettlesBusyAndLateRejectionCannotAffectNextImport() {
  val ownership=ImportOwnership(); val cancelled=ownership.begin()!!
  assertTrue(ownership.finish(cancelled)); assertFalse(ownership.busy)
  val next=ownership.begin()!!
  assertFalse(ownership.finish(cancelled)); assertTrue(ownership.busy)
  assertTrue(ownership.finish(next)); assertFalse(ownership.busy)
 }
 @Test fun exitInvalidatesPendingCallbacksAndRejectsNewImports() {
  val ownership=ImportOwnership(); val ticket=ownership.begin()!!
  ownership.dispose()
  assertFalse(ownership.isCurrent(ticket)); assertFalse(ownership.finish(ticket))
  assertFalse(ownership.busy); assertNull(ownership.begin())
 }
}
