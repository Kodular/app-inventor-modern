/**
 * @license
 *
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview Blockly React Component.
 * @author samelh@google.com (Sam El-Husseini)
 */

import { useEffect, useRef } from "react"

import * as Blockly from "blockly/core"
import * as locale from "blockly/msg/en"
import "blockly/blocks"

Blockly.setLocale(locale)

function BlocklyComponent ({
  workspaceRef,
  children,
  ...rest
}) {
  const blocklyDiv = useRef(null)

  useEffect(() => {
    if (!blocklyDiv.current) {
      console.error("blocklyDiv is not set")
      return
    }

    workspaceRef.current = Blockly.inject(blocklyDiv.current, { ...rest })

    return () => {
      workspaceRef.current?.dispose()
    }
  }, [blocklyDiv])

  return (
    <div ref={blocklyDiv} id="blocklyDiv" style={{ height: "calc(100vh - 120px)" }}/>
  )
}

export default BlocklyComponent
