import { Avatar, Group, Paper, Text, useMantineTheme } from "@mantine/core"
import { Link } from "react-router-dom"
import logo from "@/logo.png"

export default function ProjectCard ({ project }) {
  const theme = useMantineTheme()

  const secondaryColor = theme.colorScheme === "dark"
    ? theme.colors.dark[1]
    : theme.colors.gray[7]

  return (
    <Paper withBorder p="lg" radius="md" component={Link} to={"/project/" + project.id} sx={{
      '&:hover': {
        borderColor: 'lightblue'
      }
    }}>
      <Group>
        <Avatar size="lg" radius="md" style={{ backgroundColor: "lightblue" }} src={logo} alt="no image here"/>
        <div>
          <Text weight={600}>{project.name}</Text>
          <Text size="sm" style={{
            color: secondaryColor,
            lineHeight: 1.5
          }}>
            {project.description}
          </Text>
        </div>
      </Group>
    </Paper>
  )
}
