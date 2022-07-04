@file:JsNonModule
@file:JsModule("@codemirror/tooltip")

package codemirror.tooltip

import codemirror.state.Facet

/** Behavior by which an extension can provide a tooltip to be shown. */
external val showTooltip: Facet<Tooltip, Array<Tooltip>>
