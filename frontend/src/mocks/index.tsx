import { TextComponent } from "@/mocks/TextComponent"
import { ButtonComponent } from "@/mocks/ButtonComponent"
import { InputComponent } from "@/mocks/InputComponent"
import { ContainerComponent } from "@/mocks/ContainerComponent"
import { Element } from "@craftjs/core"
import React from "react"

export const mocks: Record<string, { componentElement: React.ElementType, defaultInstance: React.ReactElement }> = {
  Text: {
    componentElement: TextComponent,
    defaultInstance: <TextComponent />
  },
  Button: {
    componentElement: ButtonComponent,
    defaultInstance: <ButtonComponent />
  },
  Input: {
    componentElement: InputComponent,
    defaultInstance: <InputComponent text="Hello K2"/>
  },
  Container: {
    componentElement: ContainerComponent,
    defaultInstance: <Element canvas is={ContainerComponent} />
  },
}

export const componentCategories = {
    "Basic": ["Text", "Button", "Input"],
    "Layout": ["Container"]
}
