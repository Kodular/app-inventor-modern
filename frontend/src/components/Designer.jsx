import { Box, Grid, Group, Paper, Center } from "@mantine/core";
import { DndProvider, useDrag, useDrop } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import { components } from "../blocks/components";
import { mocks, MockScreen } from "../blocks/mocks";
import { useSelector } from "../state/store";

export default function () {
  return (
    <DndProvider backend={HTML5Backend}>
      <Grid>
        <Grid.Col span={2}><ComponentsPanel /></Grid.Col>
        <Grid.Col span={6}><LayoutPanel /></Grid.Col>
        <Grid.Col span={2}><TreePanel /></Grid.Col>
        <Grid.Col span={2}><PropertiesPanel /></Grid.Col>
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
          Object.keys(components).map((type, i) => (
            <ComponentPanelItem type={type} key={i} />
          ))
        }
      </Group>
    </div>
  )
}

function ComponentPanelItem({ type }) {

  const [collected, drag] = useDrag({
    type: "component",
    item: {
      type
    }
  })

  return (
    <Box sx={{ padding: 8, backgroundColor: 'white', border: '1px solid #eee' }} ref={drag}>
      {type}
    </Box>
  )
}

function LayoutPanel() {
  return (
    <Center>
      <Paper sx={{ height: 540, width: 320 }} shadow="xs" withBorder>
        <MockScreen />
      </Paper>
    </Center>
  )
}

export function DropZone({ vertical, parentId, order }) {
  const addComponent = useSelector(state => state.addComponent)

  const [{ isOver }, drop] = useDrop({
    accept: "component",
    collect: monitor => ({
      isOver: monitor.isOver({ shallow: true }),
    }),
    canDrop: (item, monitor) => monitor.isOver({ shallow: true }),
    drop: (item) => {
      addComponent(item, parentId, order)
    },
    // hover: (item, monitor) => {
    //   console.log('dropzone', monitor.getItem())
    // }
  })

  const style = vertical
    ? { width: '5px', height: '100%' }
    : { height: '5px', width: '100%' }

  return (
    <Box ref={drop} sx={{ ...style, backgroundColor: isOver ? 'blue' : '' }} />
  )
}

function TreeNode({ componentId }) {
  const component = useSelector(state => state.layout.components[componentId])
  const selectComponent = useSelector(state => state.selectComponent)


  function onSelect(e) {
    e.stopPropagation()
    selectComponent(componentId)
  }

  if (!component.children) {
    return <div onClick={onSelect}>{componentId}</div>
  }
  return (
    <div onClick={onSelect}>
      {componentId}
      <Box sx={{ paddingLeft: '1rem' }}>
        {
          component.children.map((childId, i) => (
            <TreeNode componentId={childId} key={i} />
          ))
        }
      </Box>
    </div>
  )
}

function TreePanel() {
  const rootNode = useSelector(state => state.layout.components[state.layout.rootId])

  return (
    <div>
      <div>Tree</div>
      <TreeNode componentId={rootNode.id} />
    </div>
  )
}

function PropertiesPanel() {
  const component = useSelector(state => state.layout.components[state.layout.selected[0]])

  if (!component) {
    return "select a component";
  }
  return (
    <div>
      <div>{component.id} Properties</div>
      <Group direction="column" grow>
        {(component.properties || []).map((property, i) => (
          <Box sx={{ padding: 8, backgroundColor: 'white', border: '1px solid #eee' }} key={i}>
            {property.name}
          </Box>
        ))}
      </Group>
    </div>
  )
}