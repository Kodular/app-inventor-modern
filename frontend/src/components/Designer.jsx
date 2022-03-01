import { Box, Grid, Group, Paper, Center } from "@mantine/core";
import { useId, useListState } from "@mantine/hooks";
import { useEffect, useState } from "react";
import { DndProvider, useDrag, useDrop } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import { components } from "../blocks/components";
import { mocks } from "../blocks/mocks";

const testlayout = [
  components.button,
  components.input,
  {
    ...components.column, children: [
      components.button,
      components.input,
    ]
  },
  {
    ...components.row, children: [
      components.button,
      components.input,
      {
        ...components.column, children: [
          components.button,
        ],
      }
    ]
  }
]

export default function () {
  const [selectedComponent, setSelectedComponent] = useState(null)
  return (
    <DndProvider backend={HTML5Backend}>
      <Grid>
        <Grid.Col span={2}><ComponentsPanel /></Grid.Col>
        <Grid.Col span={6}><LayoutPanel /></Grid.Col>
        <Grid.Col span={2}><TreePanel setSelectedComponent={setSelectedComponent} layout={testlayout} /></Grid.Col>
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
          Object.keys(components).map((component, i) => (
            <ComponentPanelItem component={component} key={i} />
          ))
        }
      </Group>
    </div>
  )
}

function ComponentPanelItem({ component }) {
  const uuid = useId()

  const [{ isDragging }, drag] = useDrag({
    type: "component",
    item: {
      component,
    },
    collect: monitor => ({
      isDragging: monitor.isDragging()
    })
  })

  return (
    <Box sx={{ padding: 8, backgroundColor: 'white', border: '1px solid #eee'}} ref={drag}>
      {component}
    </Box>
  )
}

function LayoutPanel() {
  const [values, handlers] = useListState()

  useEffect(() => {
    console.log('LayoutPanel', values)
  }, [values])

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
    <Center>
      <Paper sx={{ height: 540, width: 320 }} shadow="xs" withBorder ref={drop}>
        <Group direction="column">
          {
            values.map((item, i) => {
              const MockComponent = mocks[item.component]
              return <MockComponent key={i} item={item} />
            })
          }
        </Group>
      </Paper>
    </Center>
  )
}

function NonTerminal({ item, level, setSelectedComponent }) {
  if (!item.children) {
    return <div onClick={(e) => { e.preventDefault(); e.stopPropagation(); setSelectedComponent(item) }}>{item.name}</div>
  }
  return (
    <div onClick={(e) => { e.preventDefault(); e.stopPropagation(); setSelectedComponent(item) }}>
      {item.name}
      <Box sx={{ paddingLeft: '1rem' }}>
        {
          item.children.map((child, i) => (
            <NonTerminal key={`${level}-${i}`} item={child} level={level + 1} setSelectedComponent={setSelectedComponent} />
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
          <NonTerminal key={i} item={item} level={1} setSelectedComponent={setSelectedComponent} />
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