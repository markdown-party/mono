package party.markdown

import party.markdown.rga.MutableRGA
import party.markdown.tree.MutableTree
import party.markdown.tree.TreeNodeIdentifier

class MutableMarkdownParty {
  internal val tree = MutableTree()
  internal val files = mutableMapOf<TreeNodeIdentifier, MutableRGA>()

  fun toMarkdownParty(): MarkdownParty {
    return MarkdownParty(
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
