import { AppShell, Tabs } from "@mantine/core"
import { projects } from "@/api/projects"
import Header from "@/components/Header"
import { useParams } from "react-router-dom"
import { useMemo, useState } from "react"
import { MdDesignServices } from "react-icons/md"
import { HiOutlinePuzzle } from "react-icons/hi"
import BlocksEditor from "@/components/BlocksEditor"
import Designer from "@/components/Designer"
import { initialWorkspaceJson } from "@/blocks/toolbox"

export default function () {
  const params = useParams()
  const projectId = params.id
  const project = useMemo(() => projects.find(project => project.id === projectId), [projectId])

  const [designerState, setDesignerState] = useState(null)
  const [blocksState, setBlocksState] = useState(initialWorkspaceJson)

  return (
    <AppShell
      padding="xs"
      header={{ height: 48 }}
      styles={(theme) => ({
        main: { backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[8] : theme.colors.gray[0] },
      })}
    >
      <AppShell.Header>
        <Header />
      </AppShell.Header>
      <AppShell.Main>
        {/* keepMounted is false because otherwise Blockly wouldn't fit perfectly as the container has display:none */}
        <Tabs defaultValue="designer" variant="pills" radius="xl" keepMounted={false}>
          <Tabs.List position="right">
            <Tabs.Tab value="designer" icon={<MdDesignServices />}>Designer</Tabs.Tab>
            <Tabs.Tab value="blocks" icon={<HiOutlinePuzzle />}>BlocksEditor</Tabs.Tab>
          </Tabs.List>

          <Tabs.Panel value="designer">
            <Designer editorState={designerState} onEditorStateChange={setDesignerState} />
          </Tabs.Panel>

          <Tabs.Panel value="blocks">
            <BlocksEditor workspaceState={blocksState} onWorkspaceStateChange={setBlocksState} />
          </Tabs.Panel>
        </Tabs>
      </AppShell.Main>

    </AppShell>
  )
}
