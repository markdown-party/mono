package codemirror.state

import codemirror.text.Text

/** See `https://codemirror.net/6/docs/ref/#state.ChangeSpec`. */
@JsNonModule
@JsModule("@codemirror/state")
external interface ChangeSpec {
  var from: Int
  var to: Int
  var insert: dynamic
}

fun ChangeSpec(
    from: Int,
    to: Int? = null,
    insert: String? = null,
): ChangeSpec =
    js("{}").unsafeCast<ChangeSpec>().apply {
      this.from = from
      if (to != null) this.to = to
      if (insert != null) this.insert = insert
    }

fun ChangeSpec(
    from: Int,
    to: Int? = null,
    insert: Text? = null,
): ChangeSpec =
    js("{}").unsafeCast<ChangeSpec>().apply {
      this.from = from
      if (to != null) this.to = to
      if (insert != null) this.insert = insert
    }
