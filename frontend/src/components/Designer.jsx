import { Box, Grid, Group, List } from "@mantine/core";
import { useListState } from "@mantine/hooks";
import { useEffect, useState } from "react";
import { components } from "../blocks/components";

export default function () {
  const [values, handlers] = useListState()
  const [selectedComponent, setSelectedComponent] = useState(null)
  return (
    <Grid>
      <Grid.Col span={2}><ComponentsPanel /></Grid.Col>
      <Grid.Col span={6}>layout</Grid.Col>
      <Grid.Col span={2}><TreePanel setSelectedComponent={setSelectedComponent} /></Grid.Col>
      <Grid.Col span={2}><PropertiesPanel component={selectedComponent} /></Grid.Col>
    </Grid>
  )
}

function ComponentsPanel() {
  return (
    <div>
      <div>Components</div>
      <Group direction="column" grow>
        {
          Object.keys(components).map((component, i) => (
            <Box sx={{ padding: 8, backgroundColor: 'white', border: '1px solid #eee' }} key={i}>
              {component}
            </Box>
          ))
        }
      </Group>
    </div>
  )
}

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

function NonTerminal({ item, level, setSelectedComponent }) {
  if (!item.children) {
    return <div onClick={(e) => { e.preventDefault(); e.stopPropagation(); setSelectedComponent(item) }}>{item.name}</div>
  }
  return (
    <div onClick={(e) => {e.preventDefault(); e.stopPropagation(); setSelectedComponent(item)}}>
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

function TreePanel({ layout = testlayout, setSelectedComponent }) {
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
  useEffect(() => {
    console.log(component)
  }, [component])
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