import test from 'node:test'
import assert from 'node:assert/strict'
import {
  TOUR_IDS,
  TOUR_MANIFEST,
  completionStorageKey,
  pickAutoTourId
} from '../src/tour/manifest.js'

test('defines three focused task tours with stable DOM anchors', () => {
  assert.equal(TOUR_MANIFEST.length, 3)

  for (const tour of TOUR_MANIFEST) {
    assert.ok(tour.steps.length > 0)
    assert.ok(tour.steps.length <= 8)
    assert.equal(new Set(tour.steps.map(step => step.id)).size, tour.steps.length)
    for (const step of tour.steps) {
      assert.match(step.selector, /^\[data-tour="[a-z0-9-]+"\]$/)
      assert.ok(step.route)
      assert.ok(step.content.length > 0)
    }
  }
})

test('selects an automatic tour without adding a permission dependency', () => {
  assert.equal(pickAutoTourId({ roles: ['admin'] }), TOUR_IDS.ADMIN_FIRST_PROJECT)
  assert.equal(pickAutoTourId({ permissions: ['review:project:add'] }), TOUR_IDS.ADMIN_FIRST_PROJECT)
  assert.equal(pickAutoTourId({ permissions: ['review:insight:overview'] }), TOUR_IDS.MANAGER_FIRST_INSIGHT)
  assert.equal(pickAutoTourId({ permissions: ['review:issue:list'] }), TOUR_IDS.DEVELOPER_FIRST_ISSUE)
})

test('scopes completion state by version and user', () => {
  assert.match(completionStorageKey(1), /v1:completed:1$/)
  assert.notEqual(completionStorageKey(1), completionStorageKey(2))
})
