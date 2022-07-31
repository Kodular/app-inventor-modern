import { Box, Grid, Group, Paper, Center, Button } from "@mantine/core";
import { Editor, Frame, Element, useEditor } from "@craftjs/core";
import { TextComponent, ButtonComponent, InputComponent, Container } from "@/blocks/mocks";
import { useSelector } from "@/state/store";

const comps = {
  TextComponent,
  ButtonComponent,
  InputComponent,
  Container,
}

const compdefaults = {
  'TextComponent': <TextComponent text="Hello K2" />,
  'ButtonComponent': <ButtonComponent text="Hello K2" />,
  'InputComponent': <InputComponent text="Hello K2" />,
  'Container': <Container>I'm lonely</Container>,
}


const SaveButton = () => {
  const { query } = useEditor();
  return (
    <Button
      onClick={() => console.log(JSON.parse(query.serialize(), null, 2))}
    >
      Get JSON
    </Button>
  )
}

export default function () {
  return (
    <Editor resolver={comps}>
      <Grid>
        <Grid.Col span={2}><ComponentsPanel /></Grid.Col>
        <Grid.Col span={6}><LayoutPanel /></Grid.Col>
        <Grid.Col span={2}><TreePanel /></Grid.Col>
        <Grid.Col span={2}><PropertiesPanel /></Grid.Col>
      </Grid>
    </Editor>
  )
}

function ComponentsPanel() {
  return (
    <div>
      <SaveButton />
      <div>Components</div>
      <Group direction="column" grow>
        {
          Object.keys(compdefaults).map((type, i) => (
            <ComponentPanelItem type={type} key={i} />
          ))
        }
      </Group>
    </div>
  )
}

function ComponentPanelItem({ type }) {

  const { connectors } = useEditor();

  return (
    <Box
      ref={(ref) => connectors.create(ref, compdefaults[type])}
      sx={{ padding: 8, backgroundColor: 'white', border: '1px solid #eee' }}>
      {type}
    </Box>
  )
}

function LayoutPanel() {
  return (
    <Center>
      <Paper sx={{ height: 540, width: 320 }} shadow="xs" withBorder>
        <Frame>
          <Element is={Container} canvas />
        </Frame>
      </Paper>
    </Center>
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