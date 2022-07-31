import { produce } from 'immer'

export function log(config) {
  return (set, get, api) => config(args => {
    console.groupCollapsed()
    console.log("🔴 applying", args)
    set(args)
    console.log("🔴 new state", get())
    console.groupEnd()
  }, get, api)
}

export function immer(config) {
  return (set, get, api) => config((partial, replace) => {
    const nextState = typeof partial === 'function'
      ? produce(partial)
      : partial
    return set(nextState, replace)
  }, get, api)
}

export function pipe(...fns) {
  return (x) => fns.reduce((v, f) => f(v), x)
}
