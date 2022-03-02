import { Button, Group, Input } from "@mantine/core"
import { useListState } from "@mantine/hooks"
import { useRef } from "react"
import { useDrag, useDrop } from "react-dnd"

export const mocks = {
  'button': MockButton,
  'input': MockInput,
  'row': MockRow,
  'column': MockColumn,
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
    <Button ref={drag}>Button</Button>
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
    <Input variant="default" placeholder="input" ref={drag} />
  )
}

function MockRow({ item }) {
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
    <Group ref={(node) => drag(drop(node))} spacing="xs" p={4} noWrap sx={{ minHeight: 36, minWidth: 36, border: isOver ? '2px solid blue' : '1px solid #eee', overflowX: 'auto' }}>
      {
        values.map((item, i) => {
          const MockComponent = mocks[item.name]
          return <MockComponent key={i} item={item} />
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