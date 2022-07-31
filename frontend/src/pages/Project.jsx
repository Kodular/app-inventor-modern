import { AppShell, Navbar, SimpleGrid, Avatar, Tabs, Button } from '@mantine/core';
import { projects } from '@/api/projects';
import Header from '@/components/Header';
import { useParams } from 'react-router-dom';
import { useMemo } from 'react';
import { MdDesignServices } from 'react-icons/md';
import { HiOutlinePuzzle } from 'react-icons/hi';
import BlocksEditor from '@/components/BlocksEditor';
import Designer from '@/components/Designer';

export default function () {
  const params = useParams()
  const projectId = params.id
  const project = useMemo(() => projects.find(project => project.id === projectId), [projectId])
  return (
    <AppShell
      padding="md"
      header={<Header />}
      styles={(theme) => ({
        main: { backgroundColor: theme.colorScheme === 'dark' ? theme.colors.dark[8] : theme.colors.gray[0] },
      })}
    >
      <Tabs defaultValue="designer">
        <Tabs.List position="right">
          <Tabs.Tab value="designer" icon={<MdDesignServices />}>Designer</Tabs.Tab>
          <Tabs.Tab value="blocks" icon={<HiOutlinePuzzle />}>BlocksEditor</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="designer">
          <Designer />
        </Tabs.Panel>

        <Tabs.Panel value="blocks">
          <BlocksEditor />
        </Tabs.Panel>
      </Tabs>
    </AppShell>
  )
} 