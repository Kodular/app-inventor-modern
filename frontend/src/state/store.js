import { useCallback } from 'react'
import create from 'zustand'
import { produce } from 'immer'

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
    addComponent(item, parentId, order = -1) {
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

// Utils

function log(config) {
  return (set, get, api) => config(args => {
    console.log("🔴 applying", args)
    set(args)
    console.log("🔴 new state", get().layout.components)
  }, get, api)
}

function immer(config) {
  return (set, get, api) => config((partial, replace) => {
    const nextState = typeof partial === 'function'
      ? produce(partial)
      : partial
    return set(nextState, replace)
  }, get, api)
}

function pipe(...fns) {
  return (x) => fns.reduce((v, f) => f(v), x)
}

const idMap = {}

function genComponentId(type) {
  if (!idMap[type]) {
    idMap[type] = 0
  }
  idMap[type]++
  return `${type}${idMap[type]}`
}