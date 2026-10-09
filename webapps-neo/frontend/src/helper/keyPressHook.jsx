import { useCallback, useEffect, useLayoutEffect, useRef } from 'preact/hooks'

export const useKeyPress = (keys, callback, modifier = [], node = null) => {
  const callbackRef = useRef(callback)
  useLayoutEffect(() => {
    callbackRef.current = callback
  })

  const handleKeyPress = useCallback(
    (event) => {
      if (modifier.length !== 0)
      if (keys.some((key) => event.key === key)) {
        callbackRef.current(event)
      }
    },
    [keys, modifier]
  )

  useEffect(() => {
    const targetNode = node ?? document
    targetNode &&
    targetNode.addEventListener('keydown', handleKeyPress)

    return () =>
      targetNode &&
      targetNode.removeEventListener('keydown', handleKeyPress)
  }, [handleKeyPress, node])
}