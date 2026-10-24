/*
  tabs-context.ts: what the tab bar needs to know from TabsHost: which tab
  is showing, and where each tab was left (see components/TabsHost.tsx).
  It's in its own file, like account-context.ts, so React's live reloading
  keeps working while you edit the components.
*/
import { createContext, useContext } from 'react'
import type { TabId } from './lib/tabs'

export interface TabsState {
  active: TabId | null
  /** Where tapping a tab goes: its last page, or its first page if not opened yet. */
  pathFor: (tab: TabId) => string
}

export const TabsContext = createContext<TabsState | null>(null)

export function useTabs(): TabsState {
  const value = useContext(TabsContext)
  if (!value) throw new Error('useTabs() must be used inside <TabsHost>')
  return value
}
