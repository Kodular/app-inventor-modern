import { Box, Button, Center, Grid, Paper, Stack } from "@mantine/core"
import { Editor, Element, Frame, useEditor } from "@craftjs/core"
import React, { useMemo } from "react"
import { mocks } from "@/mocks"
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
      <SaveButton/>
      <div>Components</div>
      <Stack align={"stretch"}>
        {
          Object.entries(mocks).map(([type, { defaultInstance }], i) => (
            <Button key={i}
                    ref={(ref: HTMLButtonElement) => connectors.create(ref, defaultInstance)}
            >
              {type}
            </Button>
          ))
        }
      </Stack>
    </div>
  )
}

function LayoutPanel () {
  return (
    <Center>
      <Paper sx={{
        height: 540,
        width: 320
      }} shadow="xs" withBorder>
        <Frame>
          <Element canvas is={ContainerComponent} height="100%">
          </Element>
        </Frame>
      </Paper>
    </Center>
  )
}

function TreePanel () {
  return (
    <div>
      <div>Tree</div>
      <TreeNode componentId="ROOT"/>
    </div>
  )
}

function TreeNode ({ componentId }: { componentId: string }) {
  // const component = useSelector(state => state.layout.components[componentId])
  // const selectComponent = useSelector(state => state.selectComponent)
  // get selected node
  const {
    selfNode,
    selectedDescendants,
    actions: { selectNode }
  } = useEditor((state, query) => ({
    selfNode: query.node(componentId),
    selectedDescendants: query.node(componentId) && query.node(componentId).descendants()
  }))

  function onSelect (e: React.MouseEvent<HTMLDivElement, MouseEvent>) {
    e.stopPropagation()
    selectNode(componentId)
  }

  if (!selfNode) {
    return <div onClick={onSelect}>{componentId}</div>
  }
  return (
    <div onClick={onSelect}>
      {componentId}
      <Box sx={{ paddingLeft: "1rem" }}>
        {
          selectedDescendants?.map((childId: string, i: number) => (
            <TreeNode componentId={childId} key={i}/>
          ))
        }
      </Box>
    </div>
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
    <div>
      <div>{selected.name} Properties</div>
      <Stack>
        {selected.settings && React.createElement(selected.settings)}
      </Stack>
      <Stack>
        {selected.isDeletable && <Button onClick={() => actions.delete(selected.id)}>Delete</Button>}
      </Stack>
    </div>
  )
}
