import { AppShell, Navbar, SimpleGrid, Avatar } from '@mantine/core';
import { projects } from '@/api/projects';
import ProjectCard from '@/components/ProjectCard';
import Header from '@/components/Header';

export default function Main() {
  return (
    <AppShell
      padding="md"
      header={<Header />}
      styles={(theme) => ({
        main: { backgroundColor: theme.colorScheme === 'dark' ? theme.colors.dark[8] : theme.colors.gray[0] },
      })}
    >
      <SimpleGrid cols={6}>
        {projects.map((project, i) => (
          <ProjectCard project={project} />
        ))}
      </SimpleGrid>
    </AppShell>
  )
}