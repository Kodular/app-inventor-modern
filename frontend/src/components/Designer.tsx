import { Accordion, AspectRatio, Button, Grid, List, Paper, SimpleGrid, Stack } from "@mantine/core"
import { Editor, Element, Frame, useEditor } from "@craftjs/core"
import React, { useMemo } from "react"
import { componentCategories, mocks } from "@/mocks"
import { ContainerComponent } from "@/mocks/ContainerComponent"

const SaveButton = () => {
  const { query } = useEditor()
  return (
    <Button
      onClick={() => console.log(JSON.parse(query.serialize()))}
    >
      Get JSON
    </Button>
  )
}

interface DesignerProps {
  editorState?: string,
  onEditorStateChange: (arg0: string) => void
}

function Designer({
  editorState,
  onEditorStateChange
}: DesignerProps) {
  // keep only component property on comp object
  const compResolver = useMemo(() => Object.fromEntries(Object.entries(mocks).map(([key, { componentElement }]) => [key, componentElement])), [mocks])
  return (
    <Editor resolver={compResolver} onNodesChange={(query) => onEditorStateChange(query.serialize())}>
      <Grid>
        <Grid.Col span={2}><ComponentsPanel /></Grid.Col>
        <Grid.Col span={6}><LayoutPanel editorState={editorState} /></Grid.Col>
        <Grid.Col span={2}><TreePanel /></Grid.Col>
        <Grid.Col span={2}><PropertiesPanel /></Grid.Col>
      </Grid>
    </Editor>
  )
}

function ComponentsPanel() {
  const { connectors } = useEditor()
  return (
    <div>
      <div>Components</div>
      <Accordion defaultValue="Basic">
        {
          Object.entries(componentCategories).map(([category, components]) => (
            <Accordion.Item value={category} key={category}>
              <Accordion.Control>{category}</Accordion.Control>
              <Accordion.Panel>
                <SimpleGrid cols={2}>
                  {
                    components.map((component, index) => (
                      <AspectRatio ratio={1} key={index}>
                        <Paper withBorder
                          ref={(ref: HTMLDivElement) => connectors.create(ref, mocks[component].defaultInstance)}>
                          {component}
                        </Paper>
                      </AspectRatio>
                    ))
                  }
                </SimpleGrid>
              </Accordion.Panel>
            </Accordion.Item>
          ))
        }
      </Accordion>
    </div>
  )
}

interface LayoutPanelProps {
  editorState?: string
}

function LayoutPanel({
  editorState
}: LayoutPanelProps) {

  return (
    <AspectRatio ratio={0.5} style={{ maxWidth: 320 }} mx="auto">
      <Paper shadow="xs" withBorder style={{
        height: "100%",
        width: "100%"
      }}>
        <Frame data={editorState}>
          <Element canvas is={ContainerComponent} height="100%">
          </Element>
        </Frame>
      </Paper>
    </AspectRatio>
  )
}

function TreePanel() {
  return (
    <Stack>
      <div>Layout Tree</div>
      <List listStyleType="none">
        <TreeNode componentId="ROOT" />
      </List>
      <SaveButton />
    </Stack>
  )
}

function TreeNode({ componentId }: { componentId: string }) {
  const {
    selfNode,
    descendants,
    selectedNodeId,
    actions: { selectNode }
  } = useEditor((state, query) => ({
    selfNode: query.node(componentId).get(),
    descendants: query.node(componentId)?.descendants(),
    selectedNodeId: query.getEvent("selected").last()
  }))

  function onSelect(e: React.MouseEvent<HTMLElement, MouseEvent>) {
    e.stopPropagation()
    selectNode(componentId)
  }

  if (!selfNode) return <p>Loading...</p>

  return (
    <List.Item onClick={onSelect}>
      <div style={{
        display: "flex",
        flexDirection: "column",
        // margin: -4,
        // padding: 4,
        backgroundColor: selectedNodeId === componentId ? "#4dabf733" : undefined
      }}>
        {componentId}
        <small>{selfNode.data.name}</small>
      </div>
      <List listStyleType="none" withPadding style={{ borderLeft: "2px solid #ddd" }}>
        {
          descendants?.map((childId: string, i: number) => (
            <TreeNode componentId={childId} key={i} />
          ))
        }
      </List>
    </List.Item>
  )
}

function PropertiesPanel() {
  const {
    actions,
    selected,
    isEnabled
  } = useEditor((state, query) => {
    const currentNodeId = query.getEvent("selected").last()
    let selected

    if (currentNodeId) {
      selected = {
        id: currentNodeId,
        name: state.nodes[currentNodeId].data.name,
        settings: state.nodes[currentNodeId].related?.settings,
        isDeletable: query.node(currentNodeId).isDeletable(),
      }
    }

    return {
      selected,
      isEnabled: state.options.enabled,
    }
  })

  if (!isEnabled) {
    return <p>not enabled!</p>
  }
  if (!selected) {
    return <p>select a component</p>
  }
  return (
    <Stack>
      <div>{selected.id} ({selected.name}) Properties</div>
      {selected.settings && React.createElement(selected.settings)}
      {selected.isDeletable &&
        <Button variant="outline" color="red" onClick={() => actions.delete(selected.id)}>Delete</Button>}
    </Stack>
  )
}

export default Designer
