import { AspectRatio, Button, Center, Grid, List, Paper, SimpleGrid, Stack, Tabs } from "@mantine/core"
import { Editor, Element, Frame, useEditor } from "@craftjs/core"
import React, { useMemo } from "react"
import { componentCategories, mocks } from "@/mocks"
import { ContainerComponent } from "@/mocks/ContainerComponent"
import { HiOutlinePuzzle } from "react-icons/hi"

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

export default function () {
  // keep only component property on comp object
  const compResolver = useMemo(() => Object.fromEntries(Object.entries(mocks).map(([key, { componentElement }]) => [key, componentElement])), [mocks])
  return (
    <Editor resolver={compResolver}>
      <Grid>
        <Grid.Col span={2}><ComponentsPanel/></Grid.Col>
        <Grid.Col span={6}><LayoutPanel/></Grid.Col>
        <Grid.Col span={2}><TreePanel/></Grid.Col>
        <Grid.Col span={2}><PropertiesPanel/></Grid.Col>
      </Grid>
    </Editor>
  )
}

function ComponentsPanel () {
  const { connectors } = useEditor()
  return (
    <div>
      <div>Components</div>
      <Tabs orientation="vertical" defaultValue="Basic">
        <Tabs.List>
          {
            Object.entries(componentCategories).map(([category], index) => (
              <Tabs.Tab value={category} icon={<HiOutlinePuzzle size={14}/>} key={index}>{category}</Tabs.Tab>
            ))
          }
        </Tabs.List>

        {
          Object.entries(componentCategories).map(([category, components], index) => (
            <Tabs.Panel value={category} key={index}>
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
            </Tabs.Panel>
          ))
        }
      </Tabs>
    </div>
  )
}

function LayoutPanel () {
  return (
    <AspectRatio ratio={9 / 16} sx={{ maxWidth: 320 }} mx="auto">
      <Paper shadow="xs" withBorder style={{
        height: "100%",
        width: "100%"
      }}>
        <Frame>
          <Element canvas is={ContainerComponent} height="100%">
          </Element>
        </Frame>
      </Paper>
    </AspectRatio>
  )
}

function TreePanel () {
  return (
    <Stack>
      <div>Tree</div>
      <TreeNode componentId="ROOT"/>
      <SaveButton/>
    </Stack>
  )
}

function TreeNode ({ componentId }: { componentId: string }) {
  const {
    selfNode,
    selectedDescendants,
    actions: { selectNode }
  } = useEditor((state, query) => ({
    selfNode: query.node(componentId),
    selectedDescendants: query.node(componentId) && query.node(componentId).descendants()
  }))

  function onSelect (e: React.MouseEvent<HTMLElement, MouseEvent>) {
    e.stopPropagation()
    selectNode(componentId)
  }

  if (!selfNode) {
    return <List.Item onClick={onSelect}>{componentId}</List.Item>
  }
  return (
    <List.Item onClick={onSelect}>
      {componentId}
      <List withPadding>
        {
          selectedDescendants?.map((childId: string, i: number) => (
            <TreeNode componentId={childId} key={i}/>
          ))
        }
      </List>
    </List.Item>
  )
}

function PropertiesPanel () {
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
        settings:
          state.nodes[currentNodeId].related &&
          state.nodes[currentNodeId].related.settings,
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
      <div>{selected.name} Properties</div>
      {selected.settings && React.createElement(selected.settings)}
      {selected.isDeletable &&
        <Button variant="outline" color="red" onClick={() => actions.delete(selected.id)}>Delete</Button>}
    </Stack>
  )
}
