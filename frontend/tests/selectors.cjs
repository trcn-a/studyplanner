const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const ts = require('typescript');
const root = path.resolve(__dirname, '..');
const out = fs.mkdtempSync(path.join(require('node:os').tmpdir(), 'studyplanner-selectors-'));
const files = ['features/tasks/model/types.ts', 'features/tasks/model/selectors.ts', 'shared/lib/date.ts', 'features/calendar/model/calendar.ts', 'features/calendar/model/weekSchedule.ts', 'features/analytics/model/series.ts'];
for (const file of files) {
  const dest = path.join(out, file.replace(/\.ts$/, '.js'));
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.writeFileSync(dest, ts.transpileModule(fs.readFileSync(path.join(root, 'src', file), 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText);
}
const { selectTaskGroups } = require(path.join(out, 'features/tasks/model/selectors.js'));
const task = (id, changes = {}) => ({ id, subjectId: 'subject', parentTaskId: null, title: 'Задача', description: '', status: 'TODO', priority: 'MEDIUM', dueDate: null, estimatedMinutes: null, ...changes });
const data = [task('parent', { dueDate: '2000-01-01' }), task('child', { parentTaskId: 'parent', title: 'Підготувати ЗВІТ', priority: 'HIGH', status: 'DONE' }), task('other', { status: 'DONE' })];
assert.deepEqual(selectTaskGroups(data, 'звіт', '', '').filteredRoots.map(t => t.id), ['parent']);
assert.equal(selectTaskGroups(data, '', '', '').roots.length, 2);
assert.equal(selectTaskGroups(data, '', '', '').done, 1);
assert.equal(selectTaskGroups(data, '', '', '').overdue, 1);
assert.deepEqual(selectTaskGroups(data, '', 'DONE', 'HIGH').filteredRoots.map(t => t.id), ['parent']);
assert.equal(selectTaskGroups(data, 'відсутня', '', '').filteredRoots.length, 0);
assert.equal(selectTaskGroups([], '', '', '').roots.length, 0);
console.log('7 checks passed: nested search, parent-only counts, completion, overdue, combined filters, no results, empty state');

const { monthDays, shiftMonth, localDateKey, groupTasksByDate } = require(path.join(out, 'features/calendar/model/calendar.js'));
const september = monthDays(2026, 8);
assert.equal(september.length, 42);
assert.equal(september[0].date, '2026-08-31');
assert.equal(september.filter(day => day.inMonth).length, 30);
assert.equal(new Set(september.map(day => day.date)).size, 42);
assert.equal(monthDays(2024, 1).filter(day => day.inMonth).length, 29);
assert.equal(monthDays(2025, 1).filter(day => day.inMonth).length, 28);
assert.equal(monthDays(2026, 1)[0].date, '2026-01-26');
assert.deepEqual(shiftMonth(2026, 11, 1), { year: 2027, month: 0 });
assert.deepEqual(shiftMonth(2026, 0, -1), { year: 2025, month: 11 });
assert.equal(localDateKey(new Date(2026, 8, 25, 0, 0)), '2026-09-25');
const calendarData = [task('dated', { dueDate: '2026-09-25' }), task('sub', { parentTaskId: 'dated', dueDate: '2026-09-25' }), task('undated')];
const grouped = groupTasksByDate(calendarData);
assert.deepEqual(grouped.byDate['2026-09-25'].map(task => task.id), ['dated', 'sub']);
assert.deepEqual(grouped.withoutDate.map(task => task.id), ['undated']);
assert.equal(Object.keys(groupTasksByDate([]).byDate).length, 0);
console.log('13 calendar checks passed: Monday weeks, leap years, year boundaries, local dates, subtasks, undated tasks');

const { weekDates, workloadTasks } = require(path.join(out, 'features/calendar/model/calendar.js'));
assert.deepEqual(weekDates('2027-01-01'), ['2026-12-28','2026-12-29','2026-12-30','2026-12-31','2027-01-01','2027-01-02','2027-01-03']);
assert.equal(weekDates('2026-09-27')[0], '2026-09-21');
const load = [task('p', { estimatedMinutes: 120 }), task('c', { parentTaskId: 'p', estimatedMinutes: 30 }), task('done', { status: 'DONE', estimatedMinutes: 60 }), task('unknown')];
assert.deepEqual(workloadTasks(load, load).map(t => t.id), ['c', 'unknown']);
assert.deepEqual(workloadTasks([load[0]], load), []);
console.log('4 weekly workload checks passed: year boundary, Sunday, completed tasks, parent deduplication across filters');

const { layoutDay, selectWeekSchedule } = require(path.join(out, 'features/calendar/model/weekSchedule.js'));
const block = (id, start, end) => ({ id, taskId: id, title: id, start, end, source: 'AUTO' });
const dayLayout = layoutDay('2026-10-04', [
  block('overnight', '2026-10-03T23:30:00', '2026-10-04T00:30:00'),
  block('overlap', '2026-10-04T00:15:00', '2026-10-04T01:00:00'),
  block('boundary', '2026-10-04T01:00:00', '2026-10-05T00:00:00'),
  block('outside', '2026-10-05T00:00:00', '2026-10-05T01:00:00')
]);
assert.deepEqual(dayLayout.placed.map(p => [p.block.id, p.start, p.end, p.lane]), [
  ['overnight', 0, 30, 0], ['overlap', 15, 60, 1], ['boundary', 60, 1440, 0]
]);
assert.equal(dayLayout.laneCount, 2);
assert.equal(dayLayout.weekday, 7);
assert.equal(layoutDay('2026-10-04', []).placed.length, 0);
const selection = selectWeekSchedule([
  task('parent'), task('child', { parentTaskId: 'parent' }), task('done', { status: 'DONE' }),
  task('manual', { scheduledStart: '2026-10-04T14:00:00', scheduledEnd: '2026-10-04T15:00:00' })
], null, new Date('2026-10-04T08:00:00Z'));
assert.deepEqual(selection.unscheduled.map(t => t.id), ['child']);
assert.equal(selection.blocks[0].source, 'MANUAL');
console.log('6 schedule checks passed: overlaps, midnight boundaries, Sunday, empty day, pending leaf tasks, manual blocks');
const { bucketDays, niceMax, shiftDate } = require(path.join(out, 'features/analytics/model/series.js'));
const dayRows = (count, minutes) => Array.from({ length: count }, (_, i) => ({ date: shiftDate('2026-09-01', i), actualMinutes: minutes, plannedMinutes: minutes * 2, fulfilledMinutes: minutes, completedTasks: 0 }));
assert.equal(bucketDays(dayRows(30, 10)).length, 30);
const weeks = bucketDays(dayRows(60, 10));
assert.equal(weeks.length, 9);
assert.equal(weeks[0].actual, 70);
assert.equal(weeks.reduce((sum, b) => sum + b.actual, 0), 600);
assert.equal(weeks.reduce((sum, b) => sum + b.planned, 0), 1200);
assert.equal(niceMax(0), 60);
assert.equal(niceMax(61), 120);
assert.equal(niceMax(250), 360);
assert.equal(shiftDate('2026-03-01', -1), '2026-02-28');
assert.equal(shiftDate('2026-12-31', 1), '2027-01-01');
console.log('5 analytics checks passed: daily bars, weekly grouping for long periods, sums preserved, axis maximum, date shifting');
