package party.markdown

import party.markdown.cursors.MutableCursors
import party.markdown.rga.MutableRGA
import party.markdown.tree.MutableTree
import party.markdown.tree.TreeNodeIdentifier

class MutableMarkdownParty {
  internal val cursors = MutableCursors()
  internal val tree = MutableTree()
  internal val files = mutableMapOf<TreeNodeIdentifier, MutableRGA>()

  fun toMarkdownParty(): MarkdownParty {
    return MarkdownParty(
        cursors = cursors.toCursors(),
        tree = tree.toTree(),
        documents =
            files.toMap().mapValues { (_, rga) ->
              Pair(
                  rga.toCharArray(includeRemoved = true),
                  rga.toEventIdentifierArray(includeRemoved = true),
              )
            },
    )
  }
}
