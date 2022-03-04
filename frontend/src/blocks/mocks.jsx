import { Button, Group, Input } from "@mantine/core"
import { useListState } from "@mantine/hooks"
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
      <Group direction="column" spacing={0} ref={drop} sx={{height: '100%', width: '100%'}}>
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
            <MockComponent key={order} item={childId} />,
            <DropZone key={order + 1} parentId={rootComponent.id} order={order} />
          ]
        })
      }
    </Group>
  )
}

function MockButton({ item }) {
  const [{ isDragging }, drag] = useDrag({
    type: "component",
    item,
    collect: monitor => ({
      isDragging: monitor.isDragging()
    })
  })
  return (
    <Button ref={drag} sx={{ opacity: isDragging ? 0 : 1 }}>Button</Button>
  )
}

function MockInput({ item }) {
  const [{ isDragging }, drag] = useDrag({
    type: "component",
    item,
    collect: monitor => ({
      isDragging: monitor.isDragging()
    })
  })
  return (
    <Input variant="filled" placeholder="input" ref={drag} />
  )
}

function MockRow({ item }) {

  const [{ isDragging }, drag] = useDrag({
    type: "component",
    item,
    collect: monitor => ({
      isDragging: monitor.isDragging()
    })
  })

  // const [{ isOver }, drop] = useDrop({
  //   accept: "component",
  //   collect: monitor => ({
  //     isOver: monitor.isOver({ shallow: true }),
  //   }),
  //   canDrop: (item, monitor) => monitor.isOver({ shallow: true }),
  //   drop: (item) => {
  //     handlers.append(item)
  //   }
  // })

  return (
    <Group ref={drag} spacing={0} noWrap sx={{ minHeight: 36, minWidth: 36, overflowX: 'auto', border: '1px solid #eee' }}>
      <DropZone key={0} path={[]} order={0} vertical />
      {
        item.children.map((item, i) => {
          const MockComponent = mocks[item.name]
          const path = []
          const order = i * 2 + 1
          return [
            <MockComponent key={order} item={item} path={path} order={i + 1} />,
            <DropZone key={order + 1} parentId={""} order={i + 1} vertical />
          ]
        })
      }
    </Group>
  )
}

function MockColumn({ item }) {
  const [values, handlers] = useListState(item.children)

  const [{ isDragging }, drag] = useDrag({
    type: "component",
    item,
    collect: monitor => ({
      isDragging: monitor.isDragging()
    })
  })

  const [{ isOver }, drop] = useDrop({
    accept: "component",
    collect: monitor => ({
      isOver: monitor.isOver({ shallow: true }),
    }),
    canDrop: (item, monitor) => monitor.isOver({ shallow: true }),
    drop: (item) => {
      handlers.append(item)
    }
  })

  return (
    <Group ref={(node) => drag(drop(node))} direction="column" spacing="xs" p={4} noWrap sx={{ minHeight: 36, minWidth: 36, border: isOver ? '2px solid blue' : '1px solid #eee', overflowX: 'auto' }}>
      {
        values.map((item, i) => {
          const MockComponent = mocks[item.name]
          return <MockComponent key={i} item={item} />
        })
      }
    </Group>
  )
}