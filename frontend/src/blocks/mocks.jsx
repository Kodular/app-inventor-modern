import { Button, Group, Input } from "@mantine/core"
import { useDrag, useDrop } from "react-dnd"
import { DropZone } from "../components/Designer"
import { useSelector, useStore } from "../state/store"

export const mocks = {
  'button': MockButton,
  'input': MockInput,
  'row': MockRow,
  'column': MockColumn,
}

export function MockScreen() {
  const rootComponent = useSelector(state => state.layout.components[state.layout.rootId])
  const addComponent = useSelector(state => state.addComponent)

  const [, drop] = useDrop({
    accept: "component",
    canDrop: (item, monitor) => true || monitor.isOver({ shallow: true }) && rootComponent.children.length === 0,
    // hover(item) {
    //   console.log('hovering', item)
    // },
    drop(item) {
      addComponent(item, rootComponent.id, 0)
    }
  })

  if (rootComponent.children.length === 0) {
    return (
      <Group direction="column" spacing={0} ref={drop} sx={{ height: '100%', width: '100%' }}>
      </Group>
    )
  }

  return (
    <Group direction="column" spacing={0}>
      <DropZone key={0} parentId={rootComponent.id} order={0} />
      {
        rootComponent.children.map((childId, i) => {
          const componentType = useStore.getState().layout.components[childId].type
          const MockComponent = mocks[componentType]
          const order = i * 2 + 1
          return [
            <MockComponent key={order} componentId={childId} />,
            <DropZone key={order + 1} parentId={rootComponent.id} order={order} />
          ]
        })
      }
    </Group>
  )
}

function MockButton({ componentId }) {
  const component = useSelector(state => state.layout.components[componentId], [componentId])

  const [{ isDragging }, drag] = useDrag({
    type: "component",
    item: componentId,
    collect: monitor => ({
      isDragging: monitor.isDragging()
    })
  })

  console.log('MB', componentId)

  return (
    <Button ref={drag} sx={{ opacity: isDragging ? 0 : 1 }}>{componentId}</Button>
  )
}

function MockInput({ componentId }) {
  const component = useSelector(state => state.layout.components[componentId], [componentId])

  const [{ isDragging }, drag] = useDrag({
    type: "component",
    item: componentId,
    collect: monitor => ({
      isDragging: monitor.isDragging()
    })
  })

  return (
    <Input variant="filled" placeholder={component.id} ref={drag} />
  )
}

function MockRow({ componentId }) {
  const component = useSelector(state => state.layout.components[componentId], [componentId])
  const addComponent = useSelector(state => state.addComponent)

  const [{ isDragging }, drag] = useDrag({
    type: "component",
    item: componentId,
    collect: monitor => ({
      isDragging: monitor.isDragging()
    })
  })

  const [, drop] = useDrop({
    accept: "component",
    canDrop: (item, monitor) => monitor.isOver({ shallow: true }) && component.children.length === 0,
    // hover(item) {
    //   console.log('hovering', item)
    // },
    drop(item) {
      addComponent(item, component.id, 0)
    }
  })

  if (component.children.length === 0) {
    return (
      <Group spacing={0} ref={drop} sx={{ minHeight: 36, width: '100%', overflowX: 'auto', border: '1px solid #eee' }}>
      </Group>
    )
  }

  return (
    <Group ref={drag} spacing={0} noWrap sx={{ minHeight: 36, width: '100%', overflowX: 'auto', border: '1px solid #eee' }}>
      <DropZone key={0} parentId={component.id} order={0} vertical />
      {
        component.children.map((childId, i) => {
          const componentType = useStore.getState().layout.components[childId].type
          const MockComponent = mocks[componentType]
          const order = i * 2 + 1
          return [
            <MockComponent key={order} componentId={childId} />,
            <DropZone key={order + 1} parentId={component.id} order={order} vertical />
          ]
        })
      }
    </Group>
  )
}

function MockColumn({ componentId }) {
  const component = useSelector(state => state.layout.components[componentId], [componentId])
  const addComponent = useSelector(state => state.addComponent)

  const [{ isDragging }, drag] = useDrag({
    type: "component",
    item: componentId,
    collect: monitor => ({
      isDragging: monitor.isDragging()
    })
  })

  const [, drop] = useDrop({
    accept: "component",
    canDrop: (item, monitor) => monitor.isOver({ shallow: true }) && component.children.length === 0,
    // hover(item) {
    //   console.log('hovering', item)
    // },
    drop(item) {
      addComponent(item, component.id, 0)
    }
  })

  if (component.children.length === 0) {
    return (
      <Group direction="column" spacing={0} ref={drop} sx={{ minWidth: 72, minHeight: 36, overflowX: 'auto', border: '1px solid #eee' }}>
      </Group>
    )
  }

  return (
    <Group ref={drag} direction="column" spacing={0} noWrap sx={{ minHeight: 36, minWidth: 72, border: '1px solid #eee' }}>
      <DropZone key={0} parentId={component.id} order={0} />
      {
        component.children.map((childId, i) => {
          const componentType = useStore.getState().layout.components[childId].type
          const MockComponent = mocks[componentType]
          const order = i * 2 + 1
          return [
            <MockComponent key={order} componentId={childId} />,
            <DropZone key={order + 1} parentId={component.id} order={order} />
          ]
        })
      }
    </Group>
  )
}