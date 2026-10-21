/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

var expect = require('chai').expect;
var typeUtil = require('../../lib/forms/type-util');

describe('Date type calendar validation', function () {
  [
    '0000-02-29T00:00:00',
    '0001-01-01T00:00:00',
    '0099-12-31T23:59:59',
    '1900-02-28T12:34:56',
    '2000-02-29T12:34:56',
    '2024-02-29T12:34:56',
    '2024-04-30T12:34:56',
    '9999-12-31T23:59:59',
    '2016-12-31T23:59:60',
    '2016-12-31T23:59:60.1234',
    '2024-02-29T24:00:00',
    '2024-02-29T24:00:00.',
    '2024-02-29T24:00:00.0000',
  ].forEach(function (value) {
    it('accepts and preserves ' + value, function () {
      expect(typeUtil.isType(value, 'Date')).to.eql(true);
      expect(typeUtil.convertToType(value, 'Date')).to.eql(value);
    });
  });

  ['', '.', '.1', '.12', '.123', '.1234'].forEach(function (fraction) {
    it(
      'preserves the supported fraction ' + JSON.stringify(fraction),
      function () {
        var value = '2024-02-29T12:34:56' + fraction;
        expect(typeUtil.isType(value, 'Date')).to.eql(true);
        expect(typeUtil.convertToType(value, 'Date')).to.eql(value);
      },
    );
  });

  [
    '2023-02-29T12:34:56',
    '1900-02-29T12:34:56',
    '2100-02-29T12:34:56',
    '2024-02-30T12:34:56',
    '2024-04-31T12:34:56',
    '2024-00-01T12:34:56',
    '2024-13-01T12:34:56',
    '2024-01-00T12:34:56',
    '2024-01-32T12:34:56',
    '2024-01-01T25:00:00',
    '2013-01-23T27:42:42',
    '2013-01-23T60:42:40',
    '2024-01-01T12:60:00',
    '2024-01-01T12:00:61',
    '2024-01-01T24:01:00',
    '2024-01-01T24:00:01',
    '2024-01-01T24:00:00.0001',
    '2024-01-01T12:00:00.12345',
    '2024-01-01T12:00:00Z',
    '2024-01-01T12:00:00+00:00',
    '2024-01-01T12:00:00-05:30',
    '2024-01-01T12:00:00+0000',
    '10000-01-01T12:00:00',
    '-0001-01-01T12:00:00',
  ].forEach(function (value) {
    it('rejects ' + value, function () {
      expect(typeUtil.isType(value, 'Date')).to.eql(false);
      expect(function () {
        typeUtil.convertToType(value, 'Date');
      }).to.throw("Value '" + value + "' is not of type Date");
    });
  });

  it('retains conversion whitespace trimming', function () {
    expect(typeUtil.convertToType(' 2024-02-29T12:34:56.1234 ', 'Date')).to.eql(
      '2024-02-29T12:34:56.1234',
    );
  });

  it('rejects invalid Date objects', function () {
    expect(typeUtil.isType(new Date(NaN), 'Date')).to.eql(false);
    expect(function () {
      typeUtil.convertToType(new Date(NaN), 'Date');
    }).to.throw('is not of type Date');
  });

  ['UTC', 'America/New_York', 'Asia/Kathmandu'].forEach(function (timezone) {
    it('preserves local dates in ' + timezone, function () {
      var originalTimezone = process.env.TZ;
      try {
        process.env.TZ = timezone;
        var date = new Date(2024, 1, 29, 12, 34, 56);
        expect(typeUtil.dateToString(date)).to.eql('2024-02-29T12:34:56');
        expect(typeUtil.isType(date, 'Date')).to.eql(true);
        expect(typeUtil.convertToType(date, 'Date')).to.eql(
          '2024-02-29T12:34:56',
        );

        // Local strings have no zone, including times in a host-zone DST gap.
        expect(typeUtil.isType('2024-03-10T02:30:00', 'Date')).to.eql(true);
        expect(typeUtil.convertToType('2024-03-10T02:30:00', 'Date')).to.eql(
          '2024-03-10T02:30:00',
        );
      } finally {
        if (originalTimezone === undefined) {
          delete process.env.TZ;
        } else {
          process.env.TZ = originalTimezone;
        }
      }
    });
  });
});
