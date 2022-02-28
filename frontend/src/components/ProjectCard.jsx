import { Card, Image, Text, Badge, Button, Group, Avatar, useMantineTheme } from '@mantine/core';
import { Link } from 'react-router-dom';

export default function ProjectCard({project}) {
  const theme = useMantineTheme();

  const secondaryColor = theme.colorScheme === 'dark'
    ? theme.colors.dark[1]
    : theme.colors.gray[7];

  return (
    <Card shadow="sm" padding="lg">
      <Group style={{ marginBottom: 8, marginTop: theme.spacing.sm }}>
        <Avatar src={null} alt="no image here" color="indigo" />
        <Text weight={600}>{project.name}</Text>
      </Group>

      <Text size="sm" style={{ color: secondaryColor, lineHeight: 1.5 }}>
        {project.description}
      </Text>

      <Button variant="light" color="blue" fullWidth style={{ marginTop: 14 }} component={Link} to={"/project/" + project.id}>
        Edit project
      </Button>
    </Card>
  )
}