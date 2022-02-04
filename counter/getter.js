'use strict'

const axios = require('axios').default
const unique = require('uniq')
const LocalDate = require('@js-joda/core').LocalDate

function range (start, end) {
  const ans = []
  for (let i = start; i <= end; i++) {
    ans.push(i)
  }
  return ans
}

async function byDayAndHour (request) {
  const response = await axios.get(request.host + '/api/acct_events/v1/' + request.identityId, {
    params: request.queryParams,
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + request.token
    }
  })

  return response.data.data
}

async function byMonth (request) {
  const requestDate = LocalDate.parse(request.queryParams.date).atStartOfDay()
  const currentRange = unique(range(1, requestDate.dayOfMonth()))

  const data = []
  for (const i of currentRange) {
    request.queryParams.date = requestDate.withDayOfMonth(i).toLocalDate().toString()
    const res = await byDayAndHour(request)
    data.push(res)
  }

  return data.flat()
}

function Request (host, token, identityId, category, date, hour, subCategory) {
  const request = {
    identityId: identityId,
    host: host,
    token: token,
    queryParams: {
      cat: category,
      date: date,
      hour: hour
    }
  }
  if (subCategory) {
    request.queryParams.sub_cat = subCategory
  }

  return request
}

module.exports = { byMonth, byDayAndHour, Request }
