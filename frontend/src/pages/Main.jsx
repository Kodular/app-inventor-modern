import { AppShell, Button, FileButton, Group, SegmentedControl, SimpleGrid, Stack, Table } from "@mantine/core"
import { projects } from "@/api/projects"
import ProjectCard from "@/components/ProjectCard"
import Header from "@/components/Header"
import { useState } from "react"
import { BlobReader, ZipReader, TextWriter } from "@zip.js/zip.js"

export default function Main () {
  const [viewType, setViewType] = useState("grid")

  async function importProject(file) {
    const br = new BlobReader(file)
    const zr = new ZipReader(br)
    const entries = await zr.getEntries()

    for (const entry of entries) {
      const tw = new TextWriter()
      const content = await entry.getData(tw)
      console.log(entry.filename)
      console.log(content)
    }
  }

  return (
    <AppShell
      padding="md"
      header={<Header/>}
      styles={(theme) => ({
        main: { backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[8] : theme.colors.gray[0] },
      })}
    >
      <Stack>
        <Group position="apart">
          <Group>
            <Button variant="light">
              Start new project
            </Button>
            <FileButton variant="light" onChange={importProject} accept="application/zip">
              {(props) => <Button {...props}>Import project</Button>}
            </FileButton>
          </Group>
          <SegmentedControl
            value={viewType}
            onChange={setViewType}
            data={["grid", "table"]}
          />
        </Group>

        <ProjectsView viewType={viewType}/>
      </Stack>
    </AppShell>
  )
}

function ProjectsView ({ viewType }) {

  if (viewType === "table") {
    return (
      <Table highlightOnHover>
        <thead>
        <tr>
          <th>Name</th>
          <th>Description</th>
          <th>Action</th>
        </tr>
        </thead>
        <tbody>
        {
          projects.map((project, i) => (
            <tr key={i}>
              <td>{project.name}</td>
              <td>{project.description}</td>
              <td><Button variant="subtle">Edit</Button></td>
            </tr>
          ))
        }
        </tbody>
      </Table>
    )
  }

  return (
    <SimpleGrid cols={6}>
      {
        projects.map((project, i) => (
          <ProjectCard project={project} key={i}/>
        ))
      }
    </SimpleGrid>
  )
}

