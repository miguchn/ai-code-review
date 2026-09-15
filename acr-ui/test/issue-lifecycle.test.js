import test from 'node:test'
import assert from 'node:assert/strict'
import { relabelIssueStatusOptions, RECHECKING_DISPLAY_LABEL } from '../src/views/review/issue/issueLifecycle.js'

test('relabelIssueStatusOptions maps RECHECKING to 疑似修复', () => {
  const options = [
    { value: 'AWAITING_CONFIRM', label: '待确认' },
    { value: 'RECHECKING', label: '待复核' }
  ]
  const relabeled = relabelIssueStatusOptions(options)
  assert.equal(relabeled[0].label, '待确认')
  assert.equal(relabeled[1].label, RECHECKING_DISPLAY_LABEL)
  assert.equal(options[1].label, '待复核')
})

test('relabelIssueStatusOptions treats missing input as empty list', () => {
  assert.deepEqual(relabelIssueStatusOptions(null), [])
  assert.deepEqual(relabelIssueStatusOptions(undefined), [])
})
