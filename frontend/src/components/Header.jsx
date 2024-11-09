import { AppShellHeader, Avatar, Group, Title } from "@mantine/core"
import logo from "@/logo.png"

export default function () {
  return (
    <AppShellHeader height={60} padding="xs">
      <Group>
        <Avatar src={logo} alt="logo" color="green" />
        <Title order={4}>App Inventor Modern</Title>
      </Group>
    </AppShellHeader>
  )
}
