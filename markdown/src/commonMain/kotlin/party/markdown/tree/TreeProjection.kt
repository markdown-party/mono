package party.markdown.tree

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.projections.ChangeScope
import io.github.alexandrepiveteau.echo.projections.TwoWayProjection

object TreeProjection : TwoWayProjection<MutableTree, TreeEvent, TreeChange> {

  override fun ChangeScope<TreeChange>.forward(
      model: MutableTree,
      id: EventIdentifier,
      event: TreeEvent,
  ): MutableTree {
    with(model) {
      when (event) {
        is TreeEvent.NewFile -> newFile(id)
        is TreeEvent.NewFolder -> newFolder(id)
        is TreeEvent.Move -> move(event.element, anchor = event.anchor)
        is TreeEvent.Name -> name(event.element, event.name)
        is TreeEvent.Remove -> remove(event.element)
      }
    }
    return model
  }

  override fun backward(
      model: MutableTree,
      id: EventIdentifier,
      event: TreeEvent,
      change: TreeChange,
  ): MutableTree = model.apply { backward(change) }
}
