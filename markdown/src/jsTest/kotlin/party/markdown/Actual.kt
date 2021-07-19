package party.markdown

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

@OptIn(DelicateCoroutinesApi::class)
actual fun suspendTest(f: suspend CoroutineScope.() -> Unit): dynamic {
  return GlobalScope.promise { f() }
}
