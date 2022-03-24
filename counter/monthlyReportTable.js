const { byMonth } = require('./getter')
const { table } = require('table')
const { LocalDate } = require('@js-joda/core')

async function monthlyReportTable (request) {
  const response = await byMonth(request)

  const data = [['Year', 'Month', 'Count']]
  for (const i of response) {
    data.push([i.year, i.month, i.count])
  }
  const config = {
    columnDefault: {
      width: 30
    },
    header: {
      alignment: 'left',
      content: '<<Aggregator step I>>' +
                '\n' +
                '\nHost=' + request.host +
                '\nIdentityId=' + request.identityId +
                '\nCategory=' + request.queryParams.cat +
                '\nSubcategory=' + (request.queryParams.sub_cat ? request.queryParams.sub_cat : 'N/A') +
                '\nMonth=' + LocalDate.parse(request.queryParams.date).atStartOfDay().month().name()
    }
  }

  console.log(table(data, config))
}

module.exports = { monthlyReportTable }
