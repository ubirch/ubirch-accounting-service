const { Request, byDayAndHour } = require('./getter')
const { table } = require('table')
const { LocalDate } = require('@js-joda/core')

/**
 * ╔═══════════════════════════════════════════════════╗
 * ║ <<Aggregator step I>>                             ║
 * ║                                                   ║
 * ║ Host=http://localhost:8081                        ║
 * ║ IdentityId=12539f76-c7e9-47d6-b37b-4b59380721ac   ║
 * ║ Category=verification                             ║
 * ║ Subcategory=entry-a                               ║
 * ║ Month=FEBRUARY                                    ║
 * ║ Day=4                                             ║
 * ╟────────────┬────────────┬────────────┬────────────╢
 * ║ Year       │ Month      │ Day        │ Count      ║
 * ╟────────────┼────────────┼────────────┼────────────╢
 * ║ 2022       │ 2          │ 4          │ 1000000    ║
 * ╚════════════╧════════════╧════════════╧════════════╝
 */

const config = require('./config.json')
const request = new Request(
  config.host,
  config.token,
  '12539f76-c7e9-47d6-b37b-4b59380721ac',
  'verification',
  '2022-02-04',
  '-2',
  'entry-a'
)

byDayAndHour(request)
  .then(function (response) {
    const data = [['Year', 'Month', 'Day', 'Count']]

    let globalCount = 0
    for (const i of response) {
      globalCount = globalCount + i.count
    }

    for (const i of [response[0]]) {
      data.push([i.year, i.month, i.day, globalCount])
    }

    const config = {
      columnDefault: {
        width: 10
      },
      header: {
        alignment: 'left',
        content: '<<Aggregator step I>>' +
                    '\n' +
                    '\nHost=' + request.host +
                    '\nIdentityId=' + request.identityId +
                    '\nCategory=' + request.queryParams.cat +
                    '\nSubcategory=' + request.queryParams.sub_cat +
                    '\nMonth=' + LocalDate.parse(request.queryParams.date).atStartOfDay().month().name() +
                    '\nDay=' + LocalDate.parse(request.queryParams.date).atStartOfDay().dayOfMonth()
      }
    }

    console.log(table(data, config))
  })
  .catch(function (error) {
    console.log(error)
  })
  .then(function () {
    // always executed
  })
