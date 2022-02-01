package com.ubirch.models

import javax.inject.Inject

class AcctEventCountDAO @Inject() (val byDay: AcctEventCountByDayDAO, val byHour: AcctEventCountByHourDAO)
