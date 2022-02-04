const { Request, byDayAndHour } = require('./getter')
const { table } = require('table')
const { LocalDate } = require('@js-joda/core')

/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║ <<Aggregator step I>>                                          ║
 * ║                                                                ║
 * ║ Host=http://localhost:8081                                     ║
 * ║ IdentityId=12539f76-c7e9-47d6-b37b-4b59380721ac                ║
 * ║ Category=verification                                          ║
 * ║ Subcategory=entry-a                                            ║
 * ║ Month=FEBRUARY                                                 ║
 * ║ Day=4                                                          ║
 * ║ Hour=15                                                        ║
 * ╟────────────┬────────────┬────────────┬────────────┬────────────╢
 * ║ Year       │ Month      │ Day        │ Hour       │ Count      ║
 * ╟────────────┼────────────┼────────────┼────────────┼────────────╢
 * ║ 2022       │ 2          │ 4          │ 15         │ 1000000    ║
 * ╟────────────┼────────────┼────────────┼────────────┼────────────╢
 * ║ Year       │ Month      │ Day        │ Hour       │ Count      ║
 * ╚════════════╧════════════╧════════════╧════════════╧════════════╝
 */

const config = require('./config.json')
const request = new Request(
  config.host,
  config.token,
  '12539f76-c7e9-47d6-b37b-4b59380721ac',
  'verification',
  '2022-02-04',
  '15',
  'entry-a'
)

byDayAndHour(request)
  .then(function (response) {
    const data = [['Year', 'Month', 'Day', 'Hour', 'Count']]

    for (const i of response) {
      data.push([i.year, i.month, i.day, i.hour, i.count])
    }

    data.push(['Year', 'Month', 'Day', 'Hour', 'Count'])

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
                    '\nDay=' + LocalDate.parse(request.queryParams.date).atStartOfDay().dayOfMonth() +
                    '\nHour=' + request.queryParams.hour
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
