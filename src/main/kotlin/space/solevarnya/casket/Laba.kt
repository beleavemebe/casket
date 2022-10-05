package space.solevarnya.casket

import space.solevarnya.casket.raw.RawStudentsDao
import space.solevarnya.casket.students.StudentsDao
import space.solevarnya.casket.students.StudentEntity
import space.solevarnya.casket.tickets.TicketsDao
import space.solevarnya.casket.tickets.TicketEntity
import space.solevarnya.casket.tickets_assigned.AssignedTicketEntity
import space.solevarnya.casket.tickets_assigned.AssignedTicketsDao
import space.solevarnya.casket.tickets_assigned_and_formatted.FormattedAssignedTicketEntity
import space.solevarnya.casket.tickets_assigned_and_formatted.FormattedAssignedTicketsDao
import kotlin.random.Random

fun main() {
    // Считаем names.txt
    val rawStudentsDao = Casket.obtainDao<RawStudentsDao>()
    val rawStudents = rawStudentsDao.readAll()
    rawStudents.forEach(::println)

    // Распределим айдишники
    val studentsDao = Casket.obtainDao<StudentsDao>()
    rawStudents.forEachIndexed { index, rawStudent ->
        studentsDao.create(
            StudentEntity(index, rawStudent.name, rawStudent.surname, rawStudent.patronymic)
        )
    }

    // Заполним таблицу билетов
    val ticketsDao = Casket.obtainDao<TicketsDao>()
    repeat(99) { i ->
        ticketsDao.create(TicketEntity(i, randomTicketPath()))
    }

    // Создаем табличку [student_id, ticket_id]
    val assignedTicketsDao = Casket.obtainDao<AssignedTicketsDao>()
    assignTickets(studentsDao.readAll(), ticketsDao.readAll())
        .forEach(assignedTicketsDao::create)

    // Заполняем [student_name, ticket_path]
    val formattedAssignedTicketsDao = Casket.obtainDao<FormattedAssignedTicketsDao>()
    assignedTicketsDao.readAll().forEach { assignedTicketEntity ->
        formattedAssignedTicketsDao.create(
            FormattedAssignedTicketEntity(
                studentsDao.read(assignedTicketEntity.studentId).fullName,
                ticketsDao.read(assignedTicketEntity.ticketId).filePath
            )
        )
    }
    // Смотрим в файлик
    // Хотим вывести в консольку? ez
}

private fun randomTicketPath() = "/home/roman/tickets/ticket_${Random.Default.nextInt(0, 100)}.pdf"

fun assignTickets(
    studentEntities: List<StudentEntity>,
    ticketEntities: List<TicketEntity>
): List<AssignedTicketEntity> {
    val shuffledTickets = ticketEntities.shuffled()
    return studentEntities.mapIndexed { i, studentEntity ->
        AssignedTicketEntity(studentEntity.id, shuffledTickets[i].id)
    }
}
