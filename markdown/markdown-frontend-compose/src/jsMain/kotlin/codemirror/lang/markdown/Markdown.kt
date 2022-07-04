@file:JsNonModule
@file:JsModule("@codemirror/lang-markdown")

package codemirror.lang.markdown

import codemirror.state.Extension

// TODO : Provide more configuration ?
external fun markdown(config: dynamic = definedExternally): Extension
