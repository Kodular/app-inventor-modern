import { AppShell, Button, Group, SegmentedControl, SimpleGrid, Stack, Table } from "@mantine/core"
import { projects } from "@/api/projects"
import ProjectCard from "@/components/ProjectCard"
import Header from "@/components/Header"
import { useState } from "react"

export default function Main() {
  const [viewType, setViewType] = useState("grid")

  return (
    <AppShell
      padding="md"
      header={{ height: 48 }}
      styles={(theme) => ({
        main: { backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[8] : theme.colors.gray[0] },
      })}
    >
      <AppShell.Header>
        <Header />
      </AppShell.Header>
      <AppShell.Main>
        <Stack>
          <Group position="apart">
            <Group>
              <Button variant="light">
                Start new project
              </Button>
              <Button variant="light">
                Import project
              </Button>
            </Group>
            <SegmentedControl
              value={viewType}
              onChange={setViewType}
              data={["grid", "table"]}
            />
          </Group>

          <ProjectsView viewType={viewType} />
        </Stack>
      </AppShell.Main>
    </AppShell>
  )
}

function ProjectsView({ viewType }) {

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
          <ProjectCard project={project} key={i} />
        ))
      }
    </SimpleGrid>
  )
}

