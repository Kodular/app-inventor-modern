import { Avatar, Group, Header, Title } from "@mantine/core"
import logo from "@/logo.png"

export default function () {
  return (
    <Header height={60} padding="xs">
      <Group>
        <Avatar src={logo} alt="logo" color="green" />
        <Title order={4}>App Inventor Modern</Title>
      </Group>
    </Header>
  )
}
