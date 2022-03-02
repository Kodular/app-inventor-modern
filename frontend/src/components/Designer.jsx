import { Box, Grid, Group, Paper, Center, Divider } from "@mantine/core";
import { useEffect, useReducer, useState } from "react";
import { DndProvider, useDrag, useDrop } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import { components } from "../blocks/components";
import { mocks } from "../blocks/mocks";
import * as immutable from 'object-path-immutable'

const testlayout = [
  components.button,
  components.input,
  {
    ...components.row, children: [
      components.button,
      components.input,
    ]
  },
  {
    ...components.column, children: [
      components.button,
      {
        ...components.row, children: [
          components.button,
          components.button,
        ],
      }
    ]
  }
]


function layoutReducer(layout, action) {
  console.log(action)
  switch (action.type) {
    case 'ADD_COMPONENT': {
      const { componentType, path, order } = action
      const newLayout = immutable.insert(layout, path, components[componentType], order)
      return newLayout
    }
    case 'REMOVE_COMPONENT':
      return layout.filter((item, i) => i !== action.index)
    case 'MOVE_COMPONENT':
      return move(layout, action.from, action.to)
    default:
      return layout
  }
}

export default function () {
  const [layout, dispatchLayout] = useReducer(layoutReducer, testlayout)
  const [selectedComponent, setSelectedComponent] = useState(null)

  return (
    <DndProvider backend={HTML5Backend}>
      <Grid>
        <Grid.Col span={2}><ComponentsPanel /></Grid.Col>
        <Grid.Col span={6}><LayoutPanel layout={layout} dispatchLayout={dispatchLayout} /></Grid.Col>
        <Grid.Col span={2}><TreePanel setSelectedComponent={setSelectedComponent} layout={layout} /></Grid.Col>
        <Grid.Col span={2}><PropertiesPanel component={selectedComponent} /></Grid.Col>
      </Grid>
    </DndProvider>
  )
}

function ComponentsPanel() {
  return (
    <div>
      <div>Components</div>
      <Group direction="column" grow>
        {
          Object.keys(components).map((componentType, i) => (
            <ComponentPanelItem componentType={componentType} key={i} />
          ))
        }
      </Group>
    </div>
  )
}

function ComponentPanelItem({ componentType }) {

  const [collected, drag] = useDrag({
    type: "component",
    item: {
      componentType
    }
  })

  return (
    <Box sx={{ padding: 8, backgroundColor: 'white', border: '1px solid #eee' }} ref={drag}>
      {componentType}
    </Box>
  )
}

function LayoutPanel({ layout, dispatchLayout }) {
  return (
    <Center>
      <Paper sx={{ height: 540, width: 320 }} shadow="xs" withBorder>
        <Group direction="column" spacing={0}>
          <DropZone key={0} path={[]} order={0} dispatchLayout={dispatchLayout} />
          {
            layout.map((item, i) => {
              const MockComponent = mocks[item.name]
              const path = []
              const order = i * 2 + 1
              return [
                <MockComponent key={order} item={item} path={path} order={i+1} dispatchLayout={dispatchLayout} />,
                <DropZone key={order + 1} path={path} order={i+1} dispatchLayout={dispatchLayout} />
              ]
            })
          }
        </Group>
      </Paper>
    </Center>
  )
}

function DropZone({ vertical, path, order, dispatchLayout }) {

  const [{ isOver }, drop] = useDrop({
    accept: "component",
    collect: monitor => ({
      isOver: monitor.isOver({ shallow: true }),
    }),
    canDrop: (item, monitor) => monitor.isOver({ shallow: true }),
    drop: (item) => {
      dispatchLayout({ type: 'ADD_COMPONENT', componentType: item.componentType, path, order })
    }
  })

  const style = vertical
    ? { width: '5px', height: '100%' }
    : { height: '5px', width: '100%' }

  return (
    <Box ref={drop} sx={{ ...style, backgroundColor: isOver ? 'blue' : '' }} />
  )
}

function TreeNode({ item, level, setSelectedComponent }) {
  if (!item.children) {
    return <div onClick={(e) => { e.preventDefault(); e.stopPropagation(); setSelectedComponent(item) }}>{item.name}</div>
  }
  return (
    <div onClick={(e) => { e.preventDefault(); e.stopPropagation(); setSelectedComponent(item) }}>
      {item.name}
      <Box sx={{ paddingLeft: '1rem' }}>
        {
          item.children.map((child, i) => (
            <TreeNode key={`${level}-${i}`} item={child} level={level + 1} setSelectedComponent={setSelectedComponent} />
          ))
        }
      </Box>
    </div>
  )
}

function TreePanel({ layout, setSelectedComponent }) {
  return (
    <div>
      <div>Tree</div>
      {
        layout.map((item, i) => (
          <TreeNode key={i} item={item} level={1} setSelectedComponent={setSelectedComponent} />
        ))
      }
    </div>
  )
}

function PropertiesPanel({ component }) {
  if (!component) {
    return "select a component";
  }
  return (
    <div>
      <div>{component.name} Properties</div>
      <Group direction="column" grow>
        {component.properties.map((property, i) => (
          <Box sx={{ padding: 8, backgroundColor: 'white', border: '1px solid #eee' }} key={i}>
            {property.name}
          </Box>
        ))}
      </Group>
    </div>
  )
}