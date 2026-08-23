import test from 'node:test'
import assert from 'node:assert/strict'
import { toIdParam } from '../src/views/insight/components/insightFilter.js'

test('sanitizes route and cached ID values before path parameter requests', () => {
  for (const value of [undefined, null, '', 'undefined', 'null', 0, -1, Number.NaN]) {
    assert.equal(toIdParam(value), undefined)
  }

  assert.equal(toIdParam('12'), 12)
  assert.equal(toIdParam(12), 12)
})
