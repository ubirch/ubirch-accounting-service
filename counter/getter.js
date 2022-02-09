'use strict'

const axios = require('axios').default

async function byMonth (request) {
  const response = await axios.get(request.host + '/api/acct_events/v1/' + request.identityId, {
    params: request.queryParams,
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + request.token
    }
  })

  return response.data.data
}

function Request (host, token, identityId, category, date, subCategory) {
  const request = {
    identityId: identityId,
    host: host,
    token: token,
    queryParams: {
      cat: category,
      date: date
    }
  }
  if (subCategory) {
    request.queryParams.sub_cat = subCategory
  }

  return request
}

module.exports = { byMonth, Request }
