import { useCallback } from 'react'
import create from 'zustand'
import { log, immer, pipe } from './utils'

const createStore = pipe(immer, log, create)

export const useStore = createStore(
  (set, get) => ({
    layout: {
      rootId: 'Screen1',
      selected: [],
      components: {
        'Screen1': {
          id: 'Screen1',
          type: 'Screen',
          children: []
        }
      }
    },
    addComponent(item, parentId, order = Infinity) {
      set(state => {
        const generatedId = genComponentId(item.type)
        state.layout.components[generatedId] = { ...item, id: generatedId, children: [] }
        state.layout.components[parentId].children.splice(order, 0, generatedId)
      })
    },
    removeComponent(id) {
      set(state => {
        state.layout.components = state.layout.components.filter(item => item.id !== id)
        // TODO: maybe store parentId inside nodes
        state.layout.components.forEach(c => {
          c.children = c.children.filter(child => child !== id)
        })
      })
    },
    moveComponent(id, parentId, newParentId, newIndex) {
      /* Algorithm:
      1. Find the component to move
      2. Remove it from the existing parent
      3. Add it to the new parent
      */
      set(state => {
        const component = state.layout.components.find(c => c.id === id)
        const parent = state.layout.components.find(c => c.id === parentId)
        const newParent = state.layout.components.find(c => c.id === newParentId)
        const index = parent.children.indexOf(id)
        parent.children.splice(index, 1)
        newParent.children.splice(newIndex, 0, id)
      })
    },
    selectComponent(id, multiple = false) {
      set(state => {
        if (multiple) {
          state.layout.selected.push(id)
        } else {
          state.layout.selected = [id]
        }
      })
    }
  })
)

export function useSelector(selector, deps) {
  return useStore(useCallback(selector, deps))
}

const idMap = {}

function genComponentId(type) {
  if (!idMap[type]) {
    idMap[type] = 0
  }
  idMap[type]++
  return `${type}${idMap[type]}`
}